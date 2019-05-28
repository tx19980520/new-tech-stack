package ty0207.wordladder.wordladder;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WordladderApplication {

	public static void main(String[] args) {
		SpringApplication.run(WordladderApplication.class, args);
	}
	@Bean
	public io.opentracing.Tracer jaegerTracer() {
		return new Configuration("wordladder", new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
				new Configuration.ReporterConfiguration(
						false,
						"192.168.99.100",
						6831,
						1000,
						100
				))
				.getTracer();
	}

}
