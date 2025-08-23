package com.quickpoll.backend.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.quickpoll.backend.model.Poll;
import com.quickpoll.backend.model.PollOption;
import com.quickpoll.backend.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ShareController {

    private final PollService pollService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/poll/{pollId}")
    public String handlePollRequest(@PathVariable Long pollId,
            HttpServletRequest request,
            Model model) {

        String userAgent = request.getHeader("User-Agent");

        if (isSocialMediaBot(userAgent)) {
            log.info("Bot detected for poll {}: {}", pollId, userAgent);
            return renderOGTemplate(pollId, model);
        }

        log.info("Regular user request for poll {}, redirecting to frontend", pollId);
        return "redirect:" + frontendUrl + "/poll/" + pollId;
    }

    @GetMapping("/share/poll/{pollId}")
    public String pollSharePage(@PathVariable Long pollId, Model model) {
        return renderOGTemplate(pollId, model);
    }

    private String renderOGTemplate(Long pollId, Model model) {
        Optional<Poll> pollOpt = pollService.findById(pollId);

        if (pollOpt.isEmpty()) {
            model.addAttribute("error", "Poll not found");
            return "error";
        }

        Poll poll = pollOpt.get();

        int totalVotes = poll.getOptions().stream()
                .mapToInt(PollOption::getVotes)
                .sum();

        String optionsText = poll.getOptions().stream()
                .map(PollOption::getOptionText)
                .collect(Collectors.joining(" vs "));

        String frontendPollUrl = frontendUrl + "/poll/" + pollId;

        model.addAttribute("pollId", pollId);
        model.addAttribute("question", poll.getQuestion());
        model.addAttribute("optionsText", optionsText);
        model.addAttribute("totalVotes", totalVotes);
        model.addAttribute("shareUrl", frontendPollUrl);
        model.addAttribute("qrImageUrl", baseUrl + "/share/poll/" + pollId + "/qr");
        model.addAttribute("appName", "QuickPoll");
        model.addAttribute("frontendUrl", frontendPollUrl);

        return "poll-share";
    }

    private boolean isSocialMediaBot(String userAgent) {
        if (userAgent == null)
            return false;

        String ua = userAgent.toLowerCase();
        return ua.contains("facebookexternalhit") ||
                ua.contains("whatsapp") ||
                ua.contains("twitterbot") ||
                ua.contains("linkedinbot") ||
                ua.contains("telegrambot") ||
                ua.contains("discordbot") ||
                ua.contains("slackbot") ||
                ua.contains("facebot") ||
                ua.contains("ia_archiver") ||
                ua.contains("googlebot") ||
                ua.contains("bingbot") ||
                ua.contains("crawler") ||
                ua.contains("spider") ||
                ua.contains("bot");
    }

    @GetMapping("/share/poll/{pollId}/qr")
    public ResponseEntity<byte[]> generateQRCode(@PathVariable Long pollId) {
        try {
            Optional<Poll> pollOpt = pollService.findById(pollId);
            if (pollOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String shareUrl = frontendUrl + "/poll/" + pollId;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(shareUrl, BarcodeFormat.QR_CODE, 400, 400);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeBytes.length);
            headers.add("Cache-Control", "max-age=3600");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(qrCodeBytes);

        } catch (Exception e) {
            log.error("Error generating QR code for poll {}: {}", pollId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/share/poll/{pollId}/urls")
    @ResponseBody
    public ResponseEntity<?> getShareUrls(@PathVariable Long pollId) {
        Optional<Poll> pollOpt = pollService.findById(pollId);
        if (pollOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Poll poll = pollOpt.get();

        return ResponseEntity.ok(Map.of(
                "pollUrl", frontendUrl + "/poll/" + pollId,
                "creatorUrl", frontendUrl + "/poll/" + pollId + "?secret=" + poll.getSecretKey(),
                "qrCodeUrl", baseUrl + "/share/poll/" + pollId + "/qr",
                "shareUrl", baseUrl + "/poll/" + pollId,
                "whatsappUrl", baseUrl + "/poll/" + pollId 
        ));
    }
}
