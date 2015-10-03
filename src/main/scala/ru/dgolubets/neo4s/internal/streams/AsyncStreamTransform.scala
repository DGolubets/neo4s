package ru.dgolubets.neo4s.internal.streams

import akka.stream.stage._

import scala.collection.mutable


object AsyncStreamTransform {
  sealed trait Event
}

/**
 * Transforms a stream with specified async transformer.
 * @param transformer
 * @tparam In
 * @tparam Out
 */
class AsyncStreamTransform[In, Out](transformer: AsyncStreamTransformer[In, Out]) extends AsyncStage[In, Out, AsyncStreamTransform.Event] {

  type Event = AsyncStreamTransform.Event
  case class Next(data: Seq[Out]) extends Event
  case class Error(err: Throwable) extends Event

  import scala.concurrent.ExecutionContext.Implicits.global

  private val results: mutable.Queue[Out] = mutable.Queue()
  private var callback: AsyncCallback[Event] = _

  override def preStart(ctx: AsyncContext[Out, Event]): Unit = {
    callback = ctx.getAsyncCallback()
  }

  override def onAsyncInput(event: Event, ctx: AsyncContext[Out, Event]): Directive = {
    event match {
      case Next(data) =>
        // enqueue results from last chunk of date
        results.enqueue(data: _*)

        if (results.nonEmpty) {
          if (ctx.isHoldingDownstream) {
            // downstream is ready for next element
            val elem = results.dequeue()

            if (results.nonEmpty) {
              // have more results in queue so no need to pull right now
              ctx.push(elem)
            }
            else {
              if (ctx.isFinishing) ctx.pushAndFinish(elem)
              else ctx.pushAndPull(elem)
            }
          }
          else {
            ctx.ignore()
          }
        }
        else {
          if (ctx.isFinishing) {
            // we are empty and upstream is finished - finish too
            ctx.finish()
          }
          else {
            // we are empty and upstream has not finished - get more data
            ctx.pull()
          }
        }
      case Error(err) =>
        ctx.fail(err)
      case _ =>
        // we should not really get here ever
        sys.error("Unexpected event.")
    }
  }

  override def onPush(elem: In, ctx: AsyncContext[Out, Event]): UpstreamDirective = {
    transformer.transform(elem).map { results =>
      // stream can continue only when we transformed the current chunk of data
      callback.invoke(Next(results))
    } recover {
      case err => callback.invoke(Error(err))
    }
    ctx.holdUpstream()
  }

  override def onPull(ctx: AsyncContext[Out, Event]): DownstreamDirective = {
    if (results.nonEmpty) {
      // if have enqueued results - pull them from there
      val elem = results.dequeue()
      if (ctx.isFinishing && results.isEmpty)
        ctx.pushAndFinish(elem)
      else
        ctx.push(elem)
    }
    else {
      if (ctx.isHoldingUpstream) {
        // if we are holding upstream, that means downstream was a little bit slower with it's pull
        // and now we have to tell upstream to continue
        ctx.holdDownstreamAndPull()
      }
      else {
        // just hold it
        ctx.holdDownstream()
      }
    }
  }

  override def onUpstreamFinish(ctx: AsyncContext[Out, Event]): TerminationDirective = {
    ctx.absorbTermination()
  }
}


