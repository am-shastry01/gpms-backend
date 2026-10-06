package com.gpms.backend.superadmin.api;

import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.superadmin.application.SuperAdminService;
import com.gpms.backend.superadmin.application.SuperAdminView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The super-admin site. Server-rendered pages, not an API.
 *
 * Security lives in SuperAdminSecurityConfig: this whole path sits
 * behind its own filter chain and its own credentials, so there is no
 * per-method @PreAuthorize here - the chain has already refused anybody
 * who is not the super admin before a method is reached.
 */
@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/login")
    public String login() {
        return "superadmin/login";
    }

    @GetMapping
    public String dashboard(Model model) {

        List<SuperAdminView.CompanyRow> companies = superAdminService.companies();

        model.addAttribute("companies", companies);
        model.addAttribute("totalCompanies", companies.size());
        model.addAttribute("totalWarehouses",
                companies.stream().mapToLong(SuperAdminView.CompanyRow::warehouses).sum());
        model.addAttribute("totalPeople",
                companies.stream().mapToLong(SuperAdminView.CompanyRow::people).sum());
        model.addAttribute("totalGatePasses",
                companies.stream().mapToLong(SuperAdminView.CompanyRow::gatePasses).sum());

        return "superadmin/dashboard";
    }

    @GetMapping("/companies/new")
    public String newCompany(Model model) {
        model.addAttribute("form", new CompanyOnboardingForm());
        return "superadmin/company-new";
    }

    @PostMapping("/companies")
    public String createCompany(
            @Valid @ModelAttribute("form") CompanyOnboardingForm form,
            BindingResult binding,
            Model model
    ) {

        if (binding.hasErrors()) {
            return "superadmin/company-new";
        }

        try {
            UUID id = superAdminService.onboard(form);
            return "redirect:/superadmin/companies/" + id + "?created";

        } catch (ConflictException exception) {
            /*
             * Back to the form with everything still typed in. Losing a
             * filled-in onboarding form over a duplicate email would be
             * the worst moment to lose it.
             */
            model.addAttribute("error", exception.getMessage());
            return "superadmin/company-new";
        }
    }

    @GetMapping("/companies/{id}")
    public String company(@PathVariable UUID id, Model model) {
        model.addAttribute("company", superAdminService.company(id));
        return "superadmin/company-detail";
    }

    @PostMapping("/companies/{id}/warehouses")
    public String addWarehouse(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String number,
            Model model
    ) {
        try {
            superAdminService.addWarehouse(id, name, number);
            return "redirect:/superadmin/companies/" + id + "?added";

        } catch (ConflictException exception) {
            model.addAttribute("company", superAdminService.company(id));
            model.addAttribute("error", exception.getMessage());
            return "superadmin/company-detail";
        }
    }

    @PostMapping("/companies/{id}/warehouses/{warehouseId}/remove")
    public String removeWarehouse(
            @PathVariable UUID id,
            @PathVariable UUID warehouseId,
            Model model
    ) {
        try {
            superAdminService.removeWarehouse(id, warehouseId);
            return "redirect:/superadmin/companies/" + id + "?removed";

        } catch (ConflictException exception) {
            model.addAttribute("company", superAdminService.company(id));
            model.addAttribute("error", exception.getMessage());
            return "superadmin/company-detail";
        }
    }

    @PostMapping("/companies/{id}/active")
    public String setActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        superAdminService.setActive(id, active);
        return "redirect:/superadmin/companies/" + id;
    }
}
