# 🤖 TALKCRUIT – AI Interview Simulation Chatbot (Backend)

> **TalkCruit**은 이력서·자기소개서·채용공고를 분석해  
> 맞춤형 면접 질문을 생성하고, 면접 시뮬레이션 및 AI 피드백을 제공하는  
> AI 기반 **면접 시뮬레이션 서비스**입니다.

이 저장소는 TALKCRUIT의 **Spring Boot Backend** 레포지토리입니다.  
이력서 분석, 맞춤 질문 생성, 면접 시뮬레이션, 답변 피드백,  
Chroma 기반 RAG, OpenAI 기반 LLM 호출 등을 포함합니다.

---

# 🎯 프로젝트 목표

### ✔ 1) 맞춤형 면접 질문 및 시뮬레이션 제공  
사용자 이력서·자소서·채용공고를 분석해  
개인화된 면접 질문을 생성하고,  
면접관 성향/면접 유형 기반 **AI 면접 시뮬레이션** 제공.

### ✔ 2) 규칙 기반 + LLM 기반 피드백 제공  
답변에서 부족한 부분을 규칙 기반으로 탐지 후  
GPT를 활용해 자연스럽고 실질적인 피드백 제공.

---

# ✨ TalkCruit의 주요 차별점

기존 면접 앱과 달리 TalkCruit은 **다음 두 가지 핵심 기능**을 제공합니다.

### 🌱 1) 이력서 기반 맞춤형 질문 생성
- PDF/URL 업로드  
- 텍스트 추출(Tika)  
- 채용공고 크롤링(Jsoup)  
- Chroma DB + Embeddings 기반 RAG  
- 직무 기반 면접 질문 자동 생성

### 🤖 2) 규칙 기반 + LLM 피드백 제공
- 군더더기/모호한 표현/정량 지표 누락 등 자동 감지  
- GPT 기반 첨삭으로 자연스러운 답변 보완  
- PDF 리포트 제공

---

# 🛠 Tech Stack

##  프론트엔드
- HTML5  
- CSS3  
- TypeScript  
- React  
- Redux  
- Figma  

##  백엔드
- Java  
- Spring Boot  
- Spring Boot JWT   
- Apache Tika (PDF 텍스트 추출)  
- Jsoup (웹 크롤링)  
- OpenAI GPT-4o-mini  
- LangChain / FastAPI (면접 시뮬레이션 LLM 서버)
  
##  DB
- MySQL  
- Spring Boot 
- Chroma DB (벡터 저장소)  
---

# 🏗 시스템 아키텍처

| 구성 요소 | 기술 스택 / 역할 | 설명 |
|-----------|-------------------|-------|
| **Client** | React | • UI/UX <br>• 파일 업로드(PDF/URL) <br>• JWT 저장 및 전송 |
| **Backend API Server** | Spring Boot (REST API) | • 이력서 분석 <br>• 질문 생성 <br>• 피드백 생성 <br>• PDF 생성 <br>• Chroma & MySQL 연동 |
| **LLM Server** | FastAPI + LangChain | • 면접 시뮬레이션 처리 <br>• 꼬리 질문 생성 <br>• 대화 흐름 관리 |
| **Authentication** | JWT | • 로그인 시 JWT 발급 <br>• 모든 요청 헤더에 Authorization 포함 <br>• 사용자 인증 |
| **Database** | MySQL | • 사용자 정보 저장 <br>• 생성된 면접 질문 저장 <br>• 분석 기록 저장 |
| **Vector DB** | Chroma DB | • 이력서/자소서 임베딩 저장 <br>• 질문 생성 시 RAG 검색 |
| **AI Model** | OpenAI GPT | • 면접 답변 생성 <br>• 꼬리 질문 생성 <br>• 피드백 첨삭 |

---

