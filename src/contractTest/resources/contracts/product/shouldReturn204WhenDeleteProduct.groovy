import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "DELETE /api/v1/products/{id} debe retornar 204"
    request {
        method DELETE()
        url '/api/v1/products/1'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 204
    }
}


