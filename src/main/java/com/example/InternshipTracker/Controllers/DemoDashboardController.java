package com.example.InternshipTracker.Controllers;

import com.example.InternshipTracker.models.Internship;
import com.example.InternshipTracker.models.InternshipDto;
import com.example.InternshipTracker.services.DemoSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping("/demo-dashboard")
public class DemoDashboardController {
    private final DemoSessionService demoSessionService;

    public DemoDashboardController(DemoSessionService demoSessionService) {
        this.demoSessionService = demoSessionService;
    }

    @GetMapping
    public String showDemoDashboard(Model model, HttpSession session) {
        String sessionId = session.getId();
        System.out.println(sessionId);
        model.addAttribute("internships", demoSessionService.getTemporaryInternships(sessionId));
        return "dashboard/demo-internship-list";
    }
    @GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("internshipDto", new InternshipDto());
        return "dashboard/demo-create";
    }
    @PostMapping("/add")
    public String addInternship(@ModelAttribute Internship internship, HttpSession session) {
        demoSessionService.addInternship(session.getId(), internship);
        return "redirect:/demo-dashboard";
    }

    @GetMapping("/edit")
    public String showEditForm(@RequestParam int id, Model model,HttpSession session) {
        Set<Internship> internships = demoSessionService.getTemporaryInternships(session.getId());
        InternshipDto internshipDto = new InternshipDto();
        for (Internship i : internships) {
            if (i.getId() == id) {
                internshipDto.setStatus(i.getStatus());
                internshipDto.setNotes(i.getNotes());
                internshipDto.setId(id);
                internshipDto.setPosition(i.getPosition());
                internshipDto.setDeadline(i.getDeadline());
                internshipDto.setCompany(i.getCompany());
                break;
            }
        }
        model.addAttribute("internshipDto", internshipDto);
        return "dashboard/demo-edit";
    }
    @PostMapping("/edit")
    public String updateInternship(@RequestParam int id, @ModelAttribute InternshipDto internshipDto,
                                   BindingResult result, HttpSession session) {
        if (result.hasErrors()) {
            return "dashboard/demo-edit";
        }
        Set<Internship> internships = demoSessionService.getTemporaryInternships(session.getId());
        for (Internship i : internships) {
            if (i.getId() == id) {
                i.setCompany(internshipDto.getCompany());
                i.setPosition(internshipDto.getPosition());
                i.setStatus(internshipDto.getStatus());
                i.setDeadline(internshipDto.getDeadline());
                i.setNotes(internshipDto.getNotes());
                demoSessionService.updateInternship(session.getId(),i);
                break;
            }
        }
        return "redirect:/demo-dashboard";
    }

    @PostMapping("/logout")
    public String logoutDemo(HttpSession session) {
        demoSessionService.clearSession(session.getId());
        return "redirect:/login";
    }
}