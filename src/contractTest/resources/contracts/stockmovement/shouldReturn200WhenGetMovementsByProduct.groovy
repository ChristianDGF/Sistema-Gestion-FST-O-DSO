import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/v1/stock-movements/product/{id} debe retornar 200 con el historial"
    request {
        method GET()
        url '/api/v1/stock-movements/product/1'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
            content: [
                [
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
                ]
            ],
            page          : $(anyPositiveInt()),
            size          : $(anyPositiveInt()),
            totalElements : $(anyPositiveInt()),
            totalPages    : $(anyPositiveInt()),
            last          : $(anyBoolean())
        ])
    }
}
