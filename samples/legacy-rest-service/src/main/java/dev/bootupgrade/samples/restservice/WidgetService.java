package dev.bootupgrade.samples.restservice;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WidgetService {

    private final Map<Long, Widget> widgets = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Widget create(WidgetRequest request) {
        long id = nextId.getAndIncrement();
        Widget widget = new Widget(id, request.getName(), request.getQuantity());
        widgets.put(id, widget);
        return widget;
    }

    public Collection<Widget> findAll() {
        return widgets.values();
    }

    public Optional<Widget> findById(long id) {
        return Optional.ofNullable(widgets.get(id));
    }

    public boolean deleteById(long id) {
        return widgets.remove(id) != null;
    }
}
