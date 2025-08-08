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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MultiPatientSimulator {

    // Diretório base dos CSVs (ajuste se precisar)
    private static final String BASE_DIR = "."; // raiz do projeto. Ex.: "./simulador" se estiver em outra pasta

    private static final String[] CSV_FILES = {
            "dados_pac001.csv",
            "dados_pac002.csv",
            "dados_pac003.csv"
    };

    private static final String API_URL = "http://localhost:8080/api/v1/vital-signs";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Coloque seu token aqui se o POST estiver protegido
    private static final String BEARER_TOKEN = null; // ex.: "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."

    public static void main(String[] args) {
        for (String file : CSV_FILES) {
            final String path = new File(BASE_DIR, file).getPath();
            Thread t = new Thread(() -> runForFile(path), "sim-" + file);
            t.start();
        }
    }

    private static void runForFile(String csvPath) {
        File f = new File(csvPath);
        System.out.println("[SIM] Abrindo arquivo: " + f.getAbsolutePath() + " (exists=" + f.exists() + ")");
        if (!f.exists()) {
            System.err.println("[SIM][ERRO] Arquivo não encontrado: " + f.getAbsolutePath());
            return;
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // pula cabeçalho
            System.out.println("[SIM] Cabeçalho: " + header);

            String line;
            HttpClient client = HttpClient.newHttpClient();

            while ((line = reader.readLine()) != null) {
                count++;
                try {
                    String[] fields = line.split(",", -1); // -1 para manter campos vazios
                    // Esperado pelo desafio:
                    // 0: timestamp | 1: paciente_id | 2: paciente_nome | 3: paciente_cpf
                    // 4: hr | 5: spo2 | 6: pressao_sys | 7: pressao_dia | 8: temp | 9: resp_freq | 10: status

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("patientId", fields[1].trim());
                    payload.put("patientName", fields[2].trim());
                    payload.put("patientCpf", fields[3].trim());
                    payload.put("heartRate", parseIntSafe(fields[4]));
                    payload.put("oxygenSaturation", parseIntSafe(fields[5]));
                    payload.put("systolicPressure", parseIntSafe(fields[6]));
                    payload.put("diastolicPressure", parseIntSafe(fields[7]));
                    payload.put("temperature", parseDoubleSafe(fields[8]));
                    payload.put("respiratoryRate", parseIntSafe(fields[9]));
                    payload.put("status", fields[10].trim());
                    // você pode usar o timestamp do arquivo (fields[0]) se quiser
                    payload.put("timestamp", LocalDateTime.now().toString());

                    String json = objectMapper.writeValueAsString(payload);

                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json));

                    if (BEARER_TOKEN != null && !BEARER_TOKEN.isBlank()) {
                        builder.header("Authorization", "Bearer " + BEARER_TOKEN);
                    }

                    HttpRequest request = builder.build();
                    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        System.out.println("[SIM][" + f.getName() + "] #" + count + " OK " + resp.statusCode());
                    } else {
                        System.err.println("[SIM][" + f.getName() + "] #" + count + " FAIL " + resp.statusCode() + " body=" + resp.body());
                    }

                    Thread.sleep(200); // 200ms entre linhas
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

    private static Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private static Double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }
}
