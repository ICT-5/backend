plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ict"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.0.1"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.ai:spring-ai-pdf-document-reader")
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.session:spring-session-jdbc")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

//////////////승연추가
	implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.apache.poi:poi-ooxml:5.2.5")
	// ✅ Bean Validation 추가 (jakarta.validation.* 사용 가능)
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jsoup:jsoup:1.17.2")
	// HTML 본문 추출
	implementation("org.jsoup:jsoup:1.17.2")

	// 문서 텍스트 추출 (PDF/Doc/Docx 등)
	implementation("org.apache.tika:tika-core:2.9.2")
	implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")

	// OpenAI REST
	implementation ("com.squareup.okhttp3:okhttp:4.12.0")
	implementation ("com.fasterxml.jackson.core:jackson-databind:2.17.1")

	// 텍스트 유틸 (문장/청크 나누기 등에 사용하면 편함)
	implementation("org.apache.commons:commons-text:1.11.0")

	implementation("org.apache.pdfbox:pdfbox:2.0.31")
	implementation("org.apache.pdfbox:fontbox:2.0.31")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation ("org.jsoup:jsoup:1.17.2")

	//fot jwt
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")
}