package com.tangem.tangem_demo

import com.tangem.common.KeyPair
import com.tangem.common.extensions.hexToBytes
import com.tangem.operations.personalization.entities.Issuer

class Utils {

	companion object {

		fun randomInt(from: Int, to: Int): Int {
			return kotlin.random.Random.nextInt(from, to)
		}

		fun randomString(length: Int): String {
			val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
			val randomString = (1..length)
					.map { randomInt(0, charPool.size) }
					.map(charPool::get)
					.joinToString("");
			return randomString
		}

		fun issuer(): Issuer {
			val name = "TANGEM SDK"
			return Issuer(
					name = name,
					id = name + "\u0000",
					dataKeyPair = KeyPair(
							("045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f" +
									"361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd26").hexToBytes(),
							"11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
					),
					transactionKeyPair = KeyPair(
							("0484c5192e9bfa6c528a344f442137a92b89ea835bfef1d04cb4362eb906b508c5889846cfea71ba6dc7b" +
									"3120c2208df9c46127d3d85cb5cfbd1479e97133a39d8").hexToBytes(),
							"11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".hexToBytes()
					)
			)
		}
	}
}