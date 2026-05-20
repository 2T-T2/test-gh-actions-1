package tpanda.maven.plugins.gcc.command;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Command {
    private final Builder builder;
    public Command(Builder builder) {
        this.builder = builder;
    }

    public int execute(Charset charset, Log log) throws IOException, TimeoutException, InterruptedException {
        List<String> command = Collections.unmodifiableList(builder.command);

        log.info(String.join(" ", command));

        final Process process = new ProcessBuilder()
                .command(command)
                .redirectErrorStream(true)  // 標準出力を標準エラー出力にリダイレクト
                .start();

        // 標準出力をログ出力
        try (BufferedReader reader = process.inputReader(charset)) {
            String line;
            while ((line = reader.readLine()) != null)
                log.info(line);
        }

        if (!process.waitFor(30, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new TimeoutException("execute command timeout");
        }

        return process.exitValue();
    }

    public static class Builder {
        private final List<String> command;
        public Builder(String program) {
            this.command = new ArrayList<>();
            this.command.add(program);
        }

        public Builder addArgs(String args) {
            if (args == null || args.isBlank()) return this;

            boolean inQuote = false;
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < args.length(); i++) {
                char c = args.charAt(i);
                if (c == '"') {
                    inQuote = !inQuote;
                    continue;
                }
                if (!inQuote && Character.isWhitespace(c)) {
                    if (!cur.isEmpty()) {
                        command.add(cur.toString());
                        cur.setLength(0);
                    }
                } else {
                    cur.append(c);
                }
            }
            if (!cur.isEmpty()) command.add(cur.toString());

            return this;
        }

        public Builder addArgs(List<String> args) {
            for (String arg : args)
                addArgs(arg);
            return this;
        }

        public Command build() {
            return new Command(this);
        }
    }
}
