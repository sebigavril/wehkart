package com.wehkart.viewmodel

import com.wehkart.domain.ShoppingProduct
import com.wehkart.repository.InMemoryProducts.iPad
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.libs.json.Json

class BasketReadsSpec extends WordSpecLike with MustMatchers with OptionValues {

  "BasketReads" must {
    "read a ProductLike object " in {
      import com.wehkart.viewmodel.BasketReads.productReads
      val json = Json.parse(
        """
          | {
          |   "name":"iPad",
          |   "description":"makes you look cool",
          |   "price":999
          | }
          | """.stripMargin)
      val productLike = Json.fromJson(json).asOpt.value

      productLike.name mustEqual iPad.name
      productLike.description mustEqual iPad.description
      productLike.price mustEqual iPad.price
    }

    "read a BasketProduct object " in {
      import com.wehkart.viewmodel.BasketReads.basketProductReads
      val json = Json.parse(
        """
          | {
          |   "id":"1",
          |   "product": {
          |       "name":"iPad",
          |       "description":"makes you look cool",
          |       "price":999
          |    },
          |    "amount":2
          | }
          | """.stripMargin)

      val basketProduct = Json.fromJson[ShoppingProduct](json).asOpt.value

      basketProduct.id mustEqual "1"
      basketProduct.product.name mustEqual iPad.name
      basketProduct.amount mustEqual 2
    }
  }

}