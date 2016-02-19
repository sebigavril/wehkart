package com.wehkart.service

import scala.concurrent.Await
import java.util.UUID
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import com.wehkart.ActorConstants.duration
import com.wehkart.ActorProtocol._
import com.wehkart.{ExecutionContexts, ActorContextBaseSpec}
import com.wehkart.TestUtils.{expectProducts, id}
import com.wehkart.domain.ShoppingProduct
import com.wehkart.ActorConstants.timeout
import com.wehkart.repository.InMemoryProducts
import com.wehkart.repository.InMemoryProducts._
import org.scalatest.{WordSpecLike, MustMatchers}

class BasketActorSpec(_system: ActorSystem)
  extends ActorContextBaseSpec(_system)
    with WordSpecLike
    with MustMatchers{

  def this() = this(ActorSystem("BasketSpec"))

  private val iPadId = id(iPad)
  private val iPhoneId = id(iPhone)
  private val candyId = id(candy)

  "a Basket" must {

    "list all products" in {
      val basket = buildBasket

      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "add one product" in {
      val basket = buildBasket
      basket ! Add(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "add same product twice" in {
      val basket = buildBasket
      basket ! Add(iPadId, 1)
      basket ! Add(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 2)))
    }

    "return error when asked to add zero products" in {
      val basket = buildBasket
      val res = basket ? Add(iPadId, 0)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return error when asked to add a negative number of products" in {
      val basket = buildBasket
      val res = basket ? Add(iPadId, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return not enough stock when asked to add a number of products greater than the stock" in {
      val basket = buildBasket
      val res = basket ? Add(iPadId, 1000)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual StockNotEnough
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return out of stock when asked to add a product that has stock 0" in {
      val basket = buildBasket
      val res1 = basket ? Add(candyId, 1)
      val actual1 = Await.result(res1, duration).asInstanceOf[ActorMessage]
      val res2 = basket ? Add(candyId, 1)
      val actual2 = Await.result(res2, duration).asInstanceOf[ActorMessage]

      actual1 mustEqual Done
      actual2 mustEqual OutOfStock
      expectProducts(basket, Set(ShoppingProduct(candy, 1)))
    }

    "remove one product" in {
      val basket = buildBasket
      basket ! Add(iPadId, 2)
      basket ! Remove(iPadId, 1)

      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "remove all products" in {
      val basket = buildBasket
      basket ! Add(iPadId, 1)
      basket ! Add(iPhoneId, 2)
      basket ! Remove(iPadId, 1)
      basket ! Remove(iPhoneId, 99)

      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "return error when asked to remove a negative number of products" in {
      val basket = buildBasket
      basket ! Add(iPadId, 1)
      val res = basket ? Add(iPadId, -1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual InvalidAmount
      expectProducts(basket, Set(ShoppingProduct(iPad, 1)))
    }

    "do nothing when asked to remove a product that does not exist" in {
      val basket = buildBasket
      val res = basket ? Remove(iPadId, 1)
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual ProductNotInBasket
      expectProducts(basket, Set.empty[ShoppingProduct])
    }

    "remove all" in {
      val basket = buildBasket
      basket ! Add(iPadId, 1)
      basket ! Add(iPhoneId, 1)
      val res = basket ? RemoveAll
      val actual = Await.result(res, duration).asInstanceOf[ActorMessage]

      actual mustEqual Done
      expectProducts(basket, Set.empty[ShoppingProduct])
    }
  }

  private def buildBasket = {
    implicit val ec = new ExecutionContexts().ec
    val catalogActor = system.actorOf(CatalogActor.props(InMemoryProducts), s"catalog-${UUID.randomUUID()}")
    TestActorRef(BasketActor.props(1, catalogActor), s"basket-${UUID.randomUUID()}")
  }
}