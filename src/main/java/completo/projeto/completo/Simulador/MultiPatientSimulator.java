package completo.projeto.completo.Simulador;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final String BASE_DIR = ".";

    private static final String[] CSV_FILES = {
            "dados_pac001.csv",
            "dados_pac002.csv",
            "dados_pac003.csv"
    };

    // Endpoint dos sinais vitais
    private static final String VITALS_API_URL = "http://localhost:8080/api/v1/vital-signs";

    // Sem auth por enquanto
    private static final String BEARER_TOKEN = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // === Configs de ritmo/loop ===
    private static final long INTERVAL_MS = 200;     // intervalo entre linhas (por arquivo) — requisito do desafio
    private static final boolean LOOP_FOREVER = false;
    private static final long LOOP_PAUSE_MS = 1500;

    // Enviar name/cpf só no 1º envio bem-sucedido de cada patientId
    private static final boolean SEND_NAME_CPF_ONLY_ON_FIRST = true;

    // Pacientes já "semeados" (marcados somente após resposta 2xx)
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
                try { Thread.sleep(LOOP_PAUSE_MS); } catch (InterruptedException ignored) {}
            }
        } while (LOOP_FOREVER);
    }

    private static void processFileOnce(String csvPath) {
        File f = new File(csvPath);
        System.out.println("[SIM] Abrindo arquivo: " + f.getAbsolutePath() + " (exists=" + f.exists() + ")");
        if (!f.exists()) {
            System.err.println("[SIM][ERRO] Arquivo não encontrado: " + f.getAbsolutePath());
            return;
        }

        int count = 0;
        long lastSendMs = 0; // pacing por arquivo

        try (BufferedReader reader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // pula cabeçalho
            System.out.println("[SIM] Cabeçalho: " + header);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                try {
                    // 0: timestamp | 1: paciente_id | 2: paciente_nome | 3: paciente_cpf
                    // 4: hr | 5: spo2 | 6: pressao_sys | 7: pressao_dia | 8: temp | 9: resp_freq | 10: status
                    String[] fields = line.split(",", -1);

                    String patientId   = safeTrim(fields, 1);
                    if (isBlank(patientId)) {
                        System.err.println("[SIM][" + f.getName() + "] #" + count + " SKIP: patientId vazio");
                        continue;
                    }

                    String patientName = safeTrim(fields, 2);
                    String patientCpf  = safeTrim(fields, 3);

                    // logs brutos (primeiras linhas) — ajudam a ver o que veio do CSV
                    if (count <= 3) {
                        System.out.printf("[SIM][%s] #%d RAW hr='%s' spo2='%s' pSys='%s' pDia='%s' temp='%s' rFr='%s'%n",
                                f.getName(), count,
                                safeTrim(fields, 4), safeTrim(fields, 5), safeTrim(fields, 6),
                                safeTrim(fields, 7), safeTrim(fields, 8), safeTrim(fields, 9));
                    }

                    String ts     = normalizeTimestamp(safeTrim(fields, 0));
                    Integer hr    = parseIntSafe(safeTrim(fields, 4));
                    Integer spo2  = parseIntSafe(safeTrim(fields, 5));
                    Integer pSys  = parseIntSafe(safeTrim(fields, 6));
                    Integer pDia  = parseIntSafe(safeTrim(fields, 7));
                    Double  temp  = parseDoubleSafe(safeTrim(fields, 8));
                    Integer rFr   = parseIntSafe(safeTrim(fields, 9));
                    String  status= safeTrim(fields, 10);

                    if (count <= 3) {
                        System.out.printf("[SIM][%s] #%d PARSED hr=%s spo2=%s pSys=%s pDia=%s temp=%s rFr=%s%n",
                                f.getName(), count,
                                String.valueOf(hr), String.valueOf(spo2),
                                String.valueOf(pSys), String.valueOf(pDia),
                                String.valueOf(temp), String.valueOf(rFr));
                    }

                    boolean alreadySeeded = FIRST_SEEN_PATIENTS.contains(patientId);
                    boolean mustSendNameCpf = !SEND_NAME_CPF_ONLY_ON_FIRST || !alreadySeeded;

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("patientId", patientId);

                    if (mustSendNameCpf) {
                        if (isBlank(patientName) || isBlank(patientCpf)) {
                            System.err.println("[SIM][" + f.getName() + "] #" + count +
                                    " SKIP: primeiro registro de " + patientId +
                                    " sem patientName/patientCpf no CSV (evitando 400).");
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
                        System.out.println("[SIM][" + f.getName() + "] #" + count + " DEBUG DTO=" +
                                objectMapper.writeValueAsString(dto));
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

                    // pacing por arquivo — garante ~200ms entre envios deste CSV
                    long now = System.currentTimeMillis();
                    long due = lastSendMs + INTERVAL_MS;
                    long sleep = due - now;
                    if (sleep > 0) {
                        try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                    }
                    lastSendMs = System.currentTimeMillis();

                    HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        System.out.println("[SIM][" + f.getName() + "] #" + count + " OK " + resp.statusCode());
                        FIRST_SEEN_PATIENTS.add(patientId);
                    } else {
                        System.err.println("[SIM][" + f.getName() + "] #" + count + " FAIL " + resp.statusCode()
                                + " body=" + resp.body() + " (patientId=" + patientId + ")");
                    }

                } catch (Exception e) {
                    System.err.println("[SIM][" + f.getName() + "] Linha #" + count + " ERRO: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("[SIM] Fim do arquivo " + f.getName() + " (linhas processadas=" + count + ")");
        } catch (Exception e) {
            System.err.println("[SIM][ERRO] Falha lendo " + f.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- Helpers ----------

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeTrim(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        String s = arr[idx];
        if (s == null) return null;
        s = s.trim();
        // remove aspas externas, se existirem
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    // normaliza string numérica: aceita 85, 85.0, 85,0; remove símbolos estranhos
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

        // BR
        try {
            var br = new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/uuuu HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, br).toString();
        } catch (Exception ignore) {}

        // ISO com 'T'
        try {
            var isoT = new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, isoT).toString();
        } catch (Exception ignore) {}

        // ISO com espaço
        try {
            var isoSpace = new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd HH:mm:ss")
                    .optionalStart().appendLiteral('.').appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(s, isoSpace).toString();
        } catch (Exception ignore) {}

        System.err.println("[SIM] timestamp inválido no CSV: '" + s + "', usando agora()");
        return LocalDateTime.now().toString();
    }
}
