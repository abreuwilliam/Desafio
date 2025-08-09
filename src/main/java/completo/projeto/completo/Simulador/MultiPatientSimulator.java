package completo.projeto.completo.Simulador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiPatientSimulator {

    private static final Logger log = LoggerFactory.getLogger(MultiPatientSimulator.class);

    private static final String BASE_DIR = ".";

    private static final String[] CSV_FILES = {
            "dados_pac001.csv",
            "dados_pac002.csv",
            "dados_pac003.csv"
    };

    private static final String VITALS_API_URL = "http://localhost:8080/api/v1/vital-signs";

    private static final String BEARER_TOKEN = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Intervalo entre envios em milissegundos
    private static final long INTERVAL_MS = 200;

    // Se true, roda para sempre, se false, roda apenas uma vez
    private static final boolean LOOP_FOREVER = true;

    private static final long LOOP_PAUSE_MS = 1500;

    private static final boolean SEND_NAME_CPF_ONLY_ON_FIRST = true;

    private static final Set<String> FIRST_SEEN_PATIENTS = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        for (String file : CSV_FILES) {
            final String path = new File(BASE_DIR, file).getPath();
            Thread t = new Thread(() -> runForFile(path), "sim-" + file);
            t.start();
        }
    }

    private static void runForFile(String csvPath) {
        do {
            processFileOnce(csvPath);
            if (LOOP_FOREVER) {
                try {
                    Thread.sleep(LOOP_PAUSE_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[SIM] Thread interrompida durante pausa: {}", ie.getMessage());
                }
            }
        } while (LOOP_FOREVER);
    }

    private static void processFileOnce(String csvPath) {
        File f = new File(csvPath);
        log.info("[SIM] Abrindo arquivo: {} (exists={})", f.getAbsolutePath(), f.exists());
        if (!f.exists()) {
            log.error("[SIM][ERRO] Arquivo não encontrado: {}", f.getAbsolutePath());
            return;
        }

        int count = 0;
        long lastSendMs = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            log.info("[SIM] Cabeçalho: {}", header);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                try {
                    String[] fields = line.split(",", -1);

                    String patientId   = safeTrim(fields, 1);
                    if (isBlank(patientId)) {
                        log.error("[SIM][{}] #{} SKIP: patientId vazio", f.getName(), count);
                        continue;
                    }

                    String patientName = safeTrim(fields, 2);
                    String patientCpf  = safeTrim(fields, 3);

                    if (count <= 3) {
                        log.info("[SIM][{}] #{} RAW hr='{}' spo2='{}' pSys='{}' pDia='{}' temp='{}' rFr='{}'",
                                f.getName(), count,
                                safeTrim(fields, 4), safeTrim(fields, 5), safeTrim(fields, 6),
                                safeTrim(fields, 7), safeTrim(fields, 8), safeTrim(fields, 9));
                    }

                    String ts = normalizeTimestamp(safeTrim(fields, 0));
                    Integer hr = parseIntSafe(safeTrim(fields, 4));
                    Double spo2 = parseDoubleSafe(safeTrim(fields, 5));
                    Double pSys = parseDoubleSafe(safeTrim(fields, 6));
                    Double pDia = parseDoubleSafe(safeTrim(fields, 7));
                    Double temp = parseDoubleSafe(safeTrim(fields, 8));
                    Integer rFr = parseIntSafe(safeTrim(fields, 9));
                    String status = safeTrim(fields, 10);

                    if (count <= 3) {
                        log.info("[SIM][{}] #{} PARSED hr={} spo2={} pSys={} pDia={} temp={} rFr={}",
                                f.getName(), count, hr, spo2, pSys, pDia, temp, rFr);
                    }

                    boolean alreadySeeded = FIRST_SEEN_PATIENTS.contains(patientId);
                    boolean mustSendNameCpf = !SEND_NAME_CPF_ONLY_ON_FIRST || !alreadySeeded;

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("patientId", patientId);

                    if (mustSendNameCpf) {
                        if (isBlank(patientName) || isBlank(patientCpf)) {
                            log.error("[SIM][{}] #{} SKIP: primeiro registro de {} sem patientName/patientCpf no CSV (evitando 400).",
                                    f.getName(), count, patientId);
                            continue;
                        }
                        dto.put("patientName", patientName);
                        dto.put("patientCpf", patientCpf);
                    }

                    dto.put("heartRate", hr);
                    dto.put("oxygenSaturation", spo2);
                    dto.put("systolicPressure", pSys);
                    dto.put("diastolicPressure", pDia);
                    dto.put("temperature", temp);
                    dto.put("respiratoryRate", rFr);
                    dto.put("status", status);
                    dto.put("timestamp", ts);

                    if (count <= 3) {
                        try {
                            log.debug("[SIM][{}] #{} DEBUG DTO={}", f.getName(), count, objectMapper.writeValueAsString(dto));
                        } catch (Exception e) {
                            log.debug("[SIM][{}] #{} Falha ao serializar DTO para debug: {}", f.getName(), count, e.getMessage());
                        }
                    }

                    String json = objectMapper.writeValueAsString(dto);

                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(VITALS_API_URL))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .timeout(Duration.ofSeconds(8))
                            .POST(HttpRequest.BodyPublishers.ofString(json));

                    if (BEARER_TOKEN != null && !BEARER_TOKEN.isBlank()) {
                        builder.header("Authorization", "Bearer " + BEARER_TOKEN);
                    }

                    long now = System.currentTimeMillis();
                    long due = lastSendMs + INTERVAL_MS;
                    long sleep = due - now;
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("[SIM] Thread interrompida enquanto aguardava intervalo: {}", ie.getMessage());
                        }
                    }
                    lastSendMs = System.currentTimeMillis();

                    HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        log.info("[SIM][{}] #{} OK {}", f.getName(), count, resp.statusCode());
                        FIRST_SEEN_PATIENTS.add(patientId);
                    } else {
                        log.error("[SIM][{}] #{} FAIL {} body={} (patientId={})",
                                f.getName(), count, resp.statusCode(), resp.body(), patientId);
                    }

                } catch (Exception e) {
                    log.error("[SIM][{}] Linha #{} ERRO: {}", f.getName(), count, e.getMessage(), e);
                }
            }
            log.info("[SIM] Fim do arquivo {} (linhas processadas={})", f.getName(), count);
        } catch (Exception e) {
            log.error("[SIM][ERRO] Falha lendo {}: {}", f.getAbsolutePath(), e.getMessage(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeTrim(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        String s = arr[idx];
        if (s == null) return null;
        s = s.trim();

        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static String normalizeNum(String s) {
        if (s == null) return null;
        String n = s.trim();
        if (n.isEmpty()) return null;
        n = n.replace(',', '.').replaceAll("[^0-9+\\-.]", "");
        return n.isEmpty() ? null : n;
    }

    private static Integer parseIntSafe(String s) {
        try {
            String n = normalizeNum(s);
            if (n == null) return null;
            if (n.contains(".")) {
                double d = Double.parseDouble(n);
                return (int) Math.round(d);
            }
            return Integer.parseInt(n);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String s) {
        try {
            String n = normalizeNum(s);
            if (n == null) return null;
            return Double.parseDouble(n);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeTimestamp(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return LocalDateTime.now().toString();

        if (s.matches("^\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$")) {
            var timeOnly = new DateTimeFormatterBuilder()
                    .appendPattern("HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            LocalTime t = LocalTime.parse(s, timeOnly);
            return LocalDateTime.of(LocalDate.now(), t).toString();
        }

        try {
            var br = new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/uuuu HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, br).toString();
        } catch (Exception ignore) {}

        try {
            var isoT = new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, isoT).toString();
        } catch (Exception ignore) {}

        try {
            var isoSpace = new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, isoSpace).toString();
        } catch (Exception ignore) {}

        log.warn("[SIM] timestamp inválido no CSV: '{}', usando agora()", s);
        return LocalDateTime.now().toString();
    }
}
