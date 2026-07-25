import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/external/v1/products/{id} debe retornar 200"
    request {
        method GET()
        url '/api/external/v1/products/1'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
            id       : $(anyNumber()),
            name     : $(anyNonEmptyString()),
            sku      : $(anyNonEmptyString()),
            category : $(anyNonEmptyString()),
            price    : $(anyNumber()),
            status   : $(anyOf('ACTIVE', 'INACTIVE', 'DISCONTINUED'))
        ])
    }
}


