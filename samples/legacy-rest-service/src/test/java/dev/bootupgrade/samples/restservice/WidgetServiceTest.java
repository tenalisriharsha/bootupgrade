package dev.bootupgrade.samples.restservice;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetServiceTest {

    private final WidgetService service = new WidgetService();

    @Test
    public void createAssignsAnIncrementingId() {
        WidgetRequest first = new WidgetRequest();
        first.setName("bolt");
        first.setQuantity(10);
        WidgetRequest second = new WidgetRequest();
        second.setName("nut");
        second.setQuantity(5);

        Widget createdFirst = service.create(first);
        Widget createdSecond = service.create(second);

        assertEquals(1L, createdFirst.getId());
        assertEquals(2L, createdSecond.getId());
        assertEquals("bolt", createdFirst.getName());
        assertEquals(10, createdFirst.getQuantity());
    }

    @Test
    public void findByIdReturnsEmptyWhenMissing() {
        Optional<Widget> result = service.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    public void findByIdReturnsTheCreatedWidget() {
        WidgetRequest request = new WidgetRequest();
        request.setName("washer");
        request.setQuantity(3);
        Widget created = service.create(request);

        Optional<Widget> found = service.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("washer", found.get().getName());
    }

    @Test
    public void deleteByIdRemovesAnExistingWidgetAndReportsTrue() {
        WidgetRequest request = new WidgetRequest();
        request.setName("screw");
        request.setQuantity(1);
        Widget created = service.create(request);

        boolean deleted = service.deleteById(created.getId());

        assertTrue(deleted);
        assertFalse(service.findById(created.getId()).isPresent());
    }

    @Test
    public void deleteByIdReportsFalseWhenNothingWasRemoved() {
        boolean deleted = service.deleteById(12345L);

        assertFalse(deleted);
    }

    @Test
    public void findAllReturnsEveryCreatedWidget() {
        WidgetRequest request = new WidgetRequest();
        request.setName("rivet");
        request.setQuantity(7);
        service.create(request);

        assertEquals(1, service.findAll().size());
    }
}
