import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/v1/products/{id} debe retornar 404 si no existe"
    request {
        method GET()
        url '/api/v1/products/99'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 404
        headers { contentType(applicationJson()) }
        body([
            message   : $(anyNonEmptyString()),
            timestamp : $(anyNonEmptyString()),
            status    : 404
        ])
    }
}



