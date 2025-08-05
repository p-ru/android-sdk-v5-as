/**
 * Copyright 2015 Ekumen www.ekumenlabs.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ekumen.rosjava_actionlib

import actionlib_msgs.GoalID
import actionlib_tutorials.FibonacciActionGoal
import actionlib_tutorials.FibonacciActionResult
import com.github.rosjava_actionlib.ActionServer
import com.github.rosjava_actionlib.ActionServerListener
import org.ros.namespace.GraphName
import org.ros.node.AbstractNodeMain
import org.ros.node.ConnectedNode
import java.util.Optional
import kotlin.concurrent.Volatile

/**
 * Class to test the actionlib server.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
class `TestActionServer.md` : AbstractNodeMain(), ActionServerListener<FibonacciActionGoal?> {
    private var `as`: ActionServer<FibonacciActionGoal, FibonacciActionFeedback, FibonacciActionResult>? =
        null

    @Volatile
    private var currentGoal: FibonacciActionGoal? = null

    override fun getDefaultNodeName(): GraphName {
        return GraphName.of("fibonacci_test_server")
    }

    override fun onStart(node: ConnectedNode) {
        var result: FibonacciActionResult
        var id: String

        `as` = ActionServer<FibonacciActionGoal, FibonacciActionFeedback, FibonacciActionResult>(
            node, "/fibonacci", FibonacciActionGoal._TYPE,
            FibonacciActionFeedback._TYPE, FibonacciActionResult._TYPE
        )

        `as`.attachListener(this)

        while (true) {
            if (currentGoal != null) {
                result = `as`!!.newResultMessage()
                result.getResult().setSequence(fibonacciSequence(currentGoal.getGoal().getOrder()))
                id = currentGoal.getGoalId().getId()
                `as`!!.setSucceed(id)
                `as`.setGoalStatus(result.getStatus(), id)
                println("Sending result...")
                `as`!!.sendResult(result)
                currentGoal = null
            }
        }
    }

    override fun goalReceived(goal: FibonacciActionGoal?) {
        println("Goal received.")
    }

    override fun cancelReceived(id: GoalID) {
        println("Cancel received.")
    }

    override fun acceptGoal(goal: FibonacciActionGoal?): Optional<Boolean>? {
        // If we don't have a goal, accept it. Otherwise, reject it.
        if (currentGoal == null) {
            currentGoal = goal
            println("Goal accepted.")
            return true
        } else {
            println("We already have a goal! New goal reject.")
            return false
        }
    }

    private fun fibonacciSequence(order: Int): IntArray {
        val fib = IntArray(order + 2)

        fib[0] = 0
        fib[1] = 1

        var i = 2
        while (i < (order + 2)) {
            fib[i] = fib[i - 1] + fib[i - 2]
            i++
        }
        return fib
    }

    /*
   * Sleep for an amount on miliseconds.
   * @param msec Number or miliseconds to sleep.
   */
    private fun sleep(msec: Long) {
        try {
            Thread.sleep(msec)
        } catch (ex: InterruptedException) {
        }
    }
}