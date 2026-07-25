import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "POST /api/v1/stock-movements debe retornar 400 si hay stock insuficiente"
    request {
        method POST()
        url '/api/v1/stock-movements'
        headers {
            header('Authorization', 'Bearer test-token')
            contentType(applicationJson())
        }
        body([
            productId    : 1,
            movementType : "OUT",
            quantity     : 999,
            userId       : "user1",
            observations : "Sale"
        ])
    }
    response {
        status 400
        headers { contentType(applicationJson()) }
        body([
            message   : $(anyNonEmptyString()),
            timestamp : $(anyNonEmptyString()),
            status    : 400
        ])
    }
}



