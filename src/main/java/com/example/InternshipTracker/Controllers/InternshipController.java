package com.example.InternshipTracker.Controllers;
import com.example.InternshipTracker.models.Internship;
import com.example.InternshipTracker.models.InternshipDto;
import com.example.InternshipTracker.services.InternshipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dashboard")
public class InternshipController {
    private final InternshipService internshipService;

    public InternshipController(InternshipService internshipService) {
        this.internshipService = internshipService;
    }
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("internshipDto", new InternshipDto());
        return "dashboard/create";
    }

    @PostMapping("/create")
    public String createInternship(@ModelAttribute Internship internship, BindingResult result) {
        if (result.hasErrors()) {
            return "dashboard/create";
        }
        internshipService.saveInternship(internship);
        return "redirect:/dashboard";
    }

    @GetMapping("/edit")
    public String showEditForm(@RequestParam int id, Model model) {
        Internship internship = internshipService.getInternshipById(id);
        if (!internship.getUser().getId().equals(internshipService.getCurrentUser().getId())) {
            return "redirect:/dashboard";
        }
        InternshipDto internshipDto = new InternshipDto();
        internshipDto.setStatus(internship.getStatus());
        internshipDto.setNotes(internship.getNotes());
        internshipDto.setId(id);
        internshipDto.setPosition(internship.getPosition());
        internshipDto.setDeadline(internship.getDeadline());
        internshipDto.setCompany(internship.getCompany());
        System.out.println(internship.getDeadline());
        model.addAttribute("internshipDto", internshipDto);
        return "dashboard/edit";
    }

    @PostMapping("/edit")
    public String updateInternship(@RequestParam int id, @ModelAttribute InternshipDto internshipDto,
                                 BindingResult result) {
        if (result.hasErrors()) {
            return "dashboard/edit";
        }
        Internship internship = internshipService.getInternshipById(id);

        internship.setCompany(internshipDto.getCompany());
        internship.setPosition(internshipDto.getPosition());
        internship.setStatus(internshipDto.getStatus());
        internship.setDeadline(internshipDto.getDeadline());
        internship.setNotes(internshipDto.getNotes());
        internshipService.saveInternship(internship);

        return "redirect:/dashboard";
    }

    @GetMapping("/delete")
    public String deleteInternship(@RequestParam int id) {
        internshipService.deleteInternship(id);
        return "redirect:/dashboard";
    }
}