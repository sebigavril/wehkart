package com.wehkart.controller

import com.wehkamp.BasketActorFactory
import com.wehkart.TestUtils._
import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts.{candy, iPad}
import com.wehkart.service.BasketService
import com.wehkart.viewmodel.BasketReads.basketProductReads
import com.wehkart.{ActorContextBaseSpec, WithNextId}
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.http.MimeTypes._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.test.WithApplication

class BasketControllerSpec extends ActorContextBaseSpec
    with PlayRunners
    with WordSpecLike
    with MustMatchers
    with OptionValues {

  private val iPadId = id(iPad)
  private val candyId = id(candy)

  private val URL = "/api/shoppingbasket/:userId"

  "a BasketController" must {
    "list all products in a basket" in WithNextId { userId =>
      new WithApplication {
        basketService.add(userId, iPadId, 10)
        val result = basketController.list(userId).apply(FakeRequest(GET, URL))
        val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(result)).asOpt.value

        status(result) mustEqual OK
        contentType(result) mustEqual Some(JSON)
        expectProducts(products, Set(ShoppingProduct(iPad, 10)))
      }
    }

    "add one product in a basket" in WithNextId { userId =>
      new WithApplication {
        val addResult = basketController.add(userId, iPadId, 1).apply(FakeRequest(POST, URL))
        status(addResult) mustEqual CREATED
        contentType(addResult) mustEqual None

        val getResult = basketController.list(userId).apply(FakeRequest(GET, URL))
        val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(getResult)).asOpt.value

        status(getResult) mustEqual OK
        contentType(getResult) mustEqual Some(JSON)
        expectProducts(products, Set(ShoppingProduct(iPad, 1)))
      }
    }

    "not add an inexistent product in a basket" in WithNextId { userId =>
      new WithApplication {
        val addResult = basketController.add(userId, "DUMMY_ID", 1).apply(FakeRequest(POST, URL))
        status(addResult) mustEqual NOT_FOUND
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"Sorry, we're out of stock for this product""""
      }
    }

    "not add more products than in stock in a basket" in WithNextId { userId =>
      new WithApplication {
        val addResult = basketController.add(userId, iPadId, Int.MaxValue).apply(FakeRequest(POST, URL))
        status(addResult) mustEqual NOT_FOUND
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"Sorry, we don't have enough stock for this product""""
      }
    }

    "not add a negative number of products in a basket" in WithNextId { userId =>
      new WithApplication {
        val addResult = basketController.add(userId, iPadId, -1).apply(FakeRequest(POST, URL))
        status(addResult) mustEqual BAD_REQUEST
        contentType(addResult) mustEqual Some(JSON)
        contentAsString(addResult) mustEqual """"You're trying to add an invalid number of products: -1""""
      }
    }

    "delete one product from the basket" in WithNextId { userId =>
      new WithApplication {
        val res = for {
          _       <- basketController.add(userId, iPadId, 1).apply(FakeRequest(POST, URL))
          delete  <- basketController.delete(userId, iPadId, 1).apply(FakeRequest(DELETE, URL))
          get     <- basketController.list(userId).apply(FakeRequest(GET, URL))
        } yield (delete, get)

        val deleteRes = res.map(_._1)
        val getRes = res.map(_._2)

        status(deleteRes) mustEqual OK
        contentType(deleteRes) mustEqual None
        val products = Json.fromJson[Set[ShoppingProduct]](contentAsJson(getRes)).asOpt.value
        expectProducts(products.filter(_.id == candyId), Set.empty[ShoppingProduct])
      }
    }

    "not delete a product that is not in the basket" in WithNextId { userId =>
      new WithApplication {
        val deleteResult = basketController.delete(userId, candyId, 1).apply(FakeRequest(DELETE, URL))

        status(deleteResult) mustEqual NOT_FOUND
        contentType(deleteResult) mustEqual Some(JSON)
        contentAsString(deleteResult) mustEqual """"You're trying to delete a product that is not your basket""""
      }
    }

    "not delete a negative number of products" in WithNextId { userId =>
      new WithApplication {
        val deleteResult = basketController.delete(userId, iPadId, -1).apply(FakeRequest(DELETE, URL))

        status(deleteResult) mustEqual BAD_REQUEST
        contentType(deleteResult) mustEqual Some(JSON)
        contentAsString(deleteResult) mustEqual """"You're trying to add an invalid number of products: -1""""
      }
    }
  }

  private def basketService = new BasketService(new BasketActorFactory(actorContext))
  private def basketController = new BasketController(basketService)

}
