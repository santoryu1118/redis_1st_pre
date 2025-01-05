package org.example

import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

class OrderServiceThreadSafeTest {

    private val service = OrderServiceThreadSafe()

    @Test
    fun `동시 주문 테스트 - Thread-safe 검증`() {
        val productName = "apple"
        val orderAmount = 8
        val threadCount = 100
        val initialStock = service.getStock(productName)

        // 동시성을 위한 스레드 풀과 CountDownLatch 생성
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    // 주문 수행
                    service.order(productName, orderAmount)
                    println("Thread $threadId 주문 정보: $productName: 1 건 ([${orderAmount}]) \n")
                } finally {
                    latch.countDown() // 스레드 작업 완료
                }
            }
        }

        // 모든 스레드의 작업 완료 대기
        latch.await()
        executor.shutdown()

        // 최종 재고 계산 및 검증
        val expectedStock = initialStock % orderAmount
        val finalStock = service.getStock(productName)

        println("Expected Stock: $expectedStock, Final Stock: $finalStock")

        // 검증
        assertEquals(expectedStock, finalStock, "재고가 예상 값과 일치하지 않습니다.")
    }
}
