package at.itkollegimst.studentenverwaltung.controller;

import at.itkollegimst.studentenverwaltung.domain.Student;
import at.itkollegimst.studentenverwaltung.services.StudentenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/web/v1/studenten")
public class StudentThymleafController {

    private StudentenService studentenService;

    public StudentThymleafController(StudentenService studentenService) {
        this.studentenService = studentenService;
    }

    @GetMapping //Wenn wir jetzt über den Browser ein GetMapping absetzen kommen wir auf die Website
    public String gibAlleStudenten(Model model) {
        //Direkt Verarbeiten und in Frontend zugreifen können duch das
        model.addAttribute("allStudents", this.studentenService.alleStudenten());
        return "allestudenten";
    }

    @GetMapping("/insert")
    public String studentenEinfuegenFormular(Model model) { //Erzeugt leeren Studenten zum eingeben
        Student student = new Student();
        model.addAttribute("student", student);

        return "studenteneinfuegen";
    }

    @PostMapping("/insert")
    public String studentEinfuegen(@Valid Student student, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "studenteneinfuegen";
        } else {
            this.studentenService.studentEinfuegen(student);
            return "redirect:/web/v1/studenten";
        }
    }
}
