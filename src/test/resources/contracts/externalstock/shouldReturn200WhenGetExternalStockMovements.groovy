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
                    id               : $(anyPositiveInt()),
                    movementType     : $(anyOf('IN', 'OUT', 'ADJUSTMENT')),
                    quantity         : $(anyPositiveInt()),
                    previousQuantity : $(anyPositiveInt()),
                    newQuantity      : $(anyPositiveInt()),
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
