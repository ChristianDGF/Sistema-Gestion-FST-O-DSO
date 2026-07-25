import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/external/v1/products/{id}/stock-movements debe retornar 200"
    request {
        method GET()
        url '/api/external/v1/products/1/stock-movements'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
            content: [
                [
                    id               : $(anyNumber()),
                    movementType     : $(anyOf('IN', 'OUT', 'ADJUSTMENT')),
                    quantity         : $(anyNumber()),
                    previousQuantity : $(anyNumber()),
                    newQuantity      : $(anyNumber()),
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


