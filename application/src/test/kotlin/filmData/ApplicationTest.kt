package filmData

import com.winterbe.expekt.should
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.awaitility.Awaitility.await
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xdescribe
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vvempati on 3/23/2017.
 */
class ApplicationTest : Spek ({
    describe("Start"){
    val vertx = Vertx.vertx()
        beforeEachTest {
            val deployed = AtomicBoolean(false)
            vertx.deployVerticle(Application::class.java.getName()) { result ->
                if (result.succeeded()) {
                    deployed.set(true)
                }
            }
            await().untilTrue(deployed)
        }
        afterEachTest {
            vertx.deploymentIDs().forEach { id ->
                val undeployed = AtomicBoolean(false)
                vertx.undeploy(id) {
                    if (it.succeeded()) undeployed.set(true)
                }
                await().atMost(2, TimeUnit.SECONDS).untilTrue(undeployed)
            }
        }
        describe("The root URL is queried") {
            it("returns a 2xx status") {
                val tested = AtomicBoolean(false)
                var value = 0
                vertx.createHttpClient().getNow(8081, "localhost", "/") { response ->
                    value = response.statusCode()
                    tested.set(true)
                }
                await().atMost(2, TimeUnit.SECONDS).untilTrue(tested)
                value.should.be.at.least(200).and.below(300)
            }
        }
        describe("listAllFilms"){
            it("returns a list of all the films when queried"){
                val tested = AtomicBoolean(false)
                vertx.createHttpClient().getNow(8081, "localhost", "/listAllFilms") { response ->
                    response.bodyHandler { buffer ->
                        val payload = JsonObject(buffer.toString("ISO-8859-1"))
                        val message = payload.getJsonObject("message")
                        message.getBoolean("success").should.equal(true)
                        tested.set(true)

                    }
                }
                await().atMost(3, TimeUnit.SECONDS).untilTrue(tested)

            }
        }
        describe("view"){
            it("displays the film information of the film that the user requests"){
                val tested = AtomicBoolean(false)
                vertx.createHttpClient().getNow(8081,"localhost","/view/film1"){response ->
                    response.bodyHandler { buffer->
                        val payload = JsonObject(buffer.toString("ISO-8859-1"))
                        val message = payload.getJsonObject("message")
                        message.getBoolean("success").should.equal(true)
                        tested.set(true)
                    }
                }
                await().atMost(3,TimeUnit.SECONDS).untilTrue(tested)
            }
        }
    }
})