package com.example.bootiful_javafx;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

// the one and only endpoint this app serves. A desktop app has no https://... to be redirected
// to, so it listens on the loopback address instead (RFC 8252), and the embedded web server is
// there for exactly this one request.
@Controller
class AuthorizationCodeRedirectController {

	private final SystemBrowserOAuth2Login login;

	AuthorizationCodeRedirectController(SystemBrowserOAuth2Login login) {
		this.login = login;
	}

	@GetMapping("/login/oauth2/code/{registrationId}")
	String signedIn(@PathVariable String registrationId, @RequestParam Map<String, String> parameters, Model model) {
		model.addAttribute("name", this.login.finish(registrationId, parameters).name());
		return "signed-in";
	}

}
