package ict.project.oauth.httpinterface.config;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Component
public class HttpInterfaceFactoryImpl implements HttpInterfaceFactory {

    @Override
    public <S> S create(Class<S> clientClass, RestClient restClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }
}
