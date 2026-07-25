import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "POST /api/v1/stock-movements debe retornar 201 al registrar"
    request {
        method POST()
        url '/api/v1/stock-movements'
        headers {
            header('Authorization', 'Bearer test-token')
            contentType(applicationJson())
        }
        body([
            productId    : 1,
            movementType : "IN",
            quantity     : 5,
            userId       : "user1",
            observations : "Restock"
        ])
    }
    response {
        status 201
        headers { contentType(applicationJson()) }
        body([
            id               : $(anyPositiveInt()),
            productId        : $(anyPositiveInt()),
            productName      : $(anyNonEmptyString()),
            productSku       : $(anyNonEmptyString()),
            movementType     : $(anyOf('IN', 'OUT', 'ADJUSTMENT')),
            quantity         : $(anyPositiveInt()),
            previousQuantity : $(anyPositiveInt()),
            newQuantity      : $(anyPositiveInt()),
            userId           : $(anyNonEmptyString()),
            observations     : $(anyNonEmptyString()),
            createdAt        : $(anyIso8601WithOffset())
        ])
    }
}
