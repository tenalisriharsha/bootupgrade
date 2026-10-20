package dev.bootupgrade.samples.restservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;

@RestController
@RequestMapping("/widgets")
public class WidgetController {

    private final WidgetService widgetService;

    public WidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Widget create(@Valid @RequestBody WidgetRequest request) {
        return widgetService.create(request);
    }

    @GetMapping
    public Collection<Widget> findAll() {
        return widgetService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Widget> findById(@PathVariable long id) {
        return widgetService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable long id) {
        return widgetService.deleteById(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
