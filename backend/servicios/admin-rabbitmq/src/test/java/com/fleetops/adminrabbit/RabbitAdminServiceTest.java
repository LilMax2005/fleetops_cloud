package com.fleetops.adminrabbit;

import com.fleetops.adminrabbit.RabbitAdminDtos.QueueRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private final RabbitAdminService service = new RabbitAdminService(admin,
            new RabbitManagementProperties("http://localhost:15672", "u", "p", "/"), RestClient.builder());

    @Test
    void crearColaConDlqConfiguraDeadLetterExchange() {
        Map<String, Object> result = service.createQueue(new QueueRequest("fleetops.pagos.queue", true, true, 60000, null, false));

        assertThat(result).containsEntry("dlq", "fleetops.pagos.dlq");
        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(admin, atLeastOnce()).declareQueue(captor.capture());
        Queue main = captor.getAllValues().stream().filter(q -> q.getName().equals("fleetops.pagos.queue")).findFirst().orElseThrow();
        assertThat(main.getArguments())
                .containsEntry("x-dead-letter-exchange", "fleetops.dlx.direct")
                .containsEntry("x-dead-letter-routing-key", "fleetops.pagos.dlq")
                .containsEntry("x-message-ttl", 60000);
    }

    @Test
    void noPermiteEliminarExchangesDelSistema() {
        assertThatThrownBy(() -> service.deleteExchange("amq.topic")).isInstanceOf(IllegalArgumentException.class);
    }
}
