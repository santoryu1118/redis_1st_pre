package org.example

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

// 주문 정보를 저장하는 데이터 클래스
data class OrderInfo(
    val productName: String,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 주문 처리를 담당하는 도메인 서비스
class OrderServiceThreadSafe {

    // 상품 DB: AtomicInteger로 각 상품의 재고를 관리하여 thread-safe 보장
    private val productDatabase = ConcurrentHashMap(
        mapOf(
            "apple" to AtomicInteger(100),
            "banana" to AtomicInteger(50),
            "orange" to AtomicInteger(75)
        )
    )

    // ThreadLocal을 사용하여 각 스레드마다 독립적인 주문 데이터 보관
    private val threadLocalOrderDatabase = ThreadLocal.withInitial {
        mutableMapOf<String, MutableList<OrderInfo>>()
    }

    // 주문 처리 메서드
    fun order(productName: String, amount: Int) {
        val stock = productDatabase[productName]
            ?: throw IllegalArgumentException("Product not found: $productName")

        // 재고 감소 (원자적 감소)
        val remainingStock = stock.updateAndGet { currentStock ->
            if (currentStock >= amount) {
                Thread.sleep(ThreadLocalRandom.current().nextLong(5, 10)) // 지연 시간 추가
                currentStock - amount
            } else {
                println("Thread ${Thread.currentThread().id} - Insufficient stock for $productName")
                currentStock // 재고 부족 시 변경하지 않음
            }
        }

        if (remainingStock >= 0) {
            // 스레드별 주문 데이터 저장
            val orderInfo = OrderInfo(productName, amount)
            threadLocalOrderDatabase.get().computeIfAbsent(productName) { mutableListOf() }.add(orderInfo)

            // 로그 출력
            println("Thread ${Thread.currentThread().id} 주문 정보: $productName: 1 건 ([${amount}])")
        }
    }

    // 재고 조회
    fun getStock(productName: String): Int {
        return productDatabase[productName]?.get() ?: 0
    }

    // 스레드별 주문 데이터 조회
    fun getThreadLocalOrderList(): Map<String, List<OrderInfo>> {
        return threadLocalOrderDatabase.get()
    }
}
