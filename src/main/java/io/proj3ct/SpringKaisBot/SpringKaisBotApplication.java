package io.proj3ct.SpringKaisBot;

import io.proj3ct.SpringKaisBot.config.BotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@SpringBootApplication
public class SpringKaisBotApplication {

	public static List<String> blacklist = new ArrayList<>();

	public static void main(String[] args) {

			try {
				blacklist = Files.readAllLines(Paths.get("list1.bmp"));
			} catch (IOException e) {
			  blacklist.add("Тупой бот");
  			  blacklist.add("Охуеть");
  			  blacklist.add(" на хуй");
			}



		SpringApplication.run(SpringKaisBotApplication.class, args);
	}

}
