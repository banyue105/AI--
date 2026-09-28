package com.ican.assistant.modules.careerplanning;

import com.ican.assistant.modules.careerplanning.CareerDtos.PreferenceInput;
import com.ican.assistant.modules.careerplanning.CareerDtos.ReportInput;
import com.ican.assistant.modules.careerplanning.CareerDtos.TargetInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/career")
public class CareerController {
    private final CareerService service;

    public CareerController(CareerService service) { this.service = service; }

    @GetMapping("/jobs")
    public Object jobs(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "") String city,
                       @RequestParam(defaultValue = "") String jobType) {
        return service.jobs(keyword, category, city, jobType);
    }

    @GetMapping("/matches")
    public Object matches() { return service.matches(); }

    @GetMapping("/preferences")
    public Object preferences() { return service.preference(); }

    @PutMapping("/preferences")
    public Object updatePreferences(@Valid @RequestBody PreferenceInput input) {
        return service.savePreference(input);
    }

    @PutMapping("/target")
    public Object target(@Valid @RequestBody TargetInput input) { return service.setTarget(input.jobId()); }

    @GetMapping("/target")
    public Object target() { return new TargetResponse(service.target()); }

    public record TargetResponse(CareerDtos.Job job) {}

    @GetMapping("/reports")
    public Object reports() { return service.reports(); }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createReport(@Valid @RequestBody ReportInput input) { return service.createReport(input); }

    @GetMapping("/reports/{id}")
    public Object report(@PathVariable String id) { return service.report(id); }
}
