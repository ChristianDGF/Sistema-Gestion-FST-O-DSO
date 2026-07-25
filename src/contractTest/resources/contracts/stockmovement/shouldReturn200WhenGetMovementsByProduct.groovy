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
                    id               : $(anyNumber()),
                    productId        : $(anyNumber()),
                    productName      : $(anyNonEmptyString()),
                    productSku       : $(anyNonEmptyString()),
                    movementType     : $(anyOf('IN', 'OUT', 'ADJUSTMENT')),
                    quantity         : $(anyNumber()),
                    previousQuantity : $(anyNumber()),
                    newQuantity      : $(anyNumber()),
                    userId           : $(anyNonEmptyString()),
                    observations     : $(anyNonEmptyString()),
                    createdAt        : $(anyNonEmptyString())
                ]
            ],
            page          : $(anyNumber()),
            size          : $(anyNumber()),
            totalElements : $(anyNumber()),
            totalPages    : $(anyNumber()),
            last          : $(anyBoolean())
        ])
    }
}


