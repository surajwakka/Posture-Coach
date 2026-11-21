# Posture & Eye-Strain Coach (Spring Boot + DJL + OpenCV)

Browser -> JPEG snapshots (1/s) over WebSocket `/ws/posture` -> Spring Boot decodes with OpenCV,
runs SimplePose (DJL/MXNet), computes shoulder tilt, screen-closeness and head-drop, and pushes
back metrics + alerts (sustained bad posture, 20-20-20 eye breaks).

## Run locally
    mvn spring-boot:run      # then open http://localhost:8080  (camera works on localhost)
## Docker
    docker build -t posture-coach . && docker run -p 8080:8080 posture-coach
## Deploy (HTTPS is required for webcam access)
Push to GitHub -> Render.com -> New Web Service -> select repo (render.yaml is picked up).
Fly.io / Railway / Google Cloud Run work with the same Dockerfile (Cloud Run: enable WebSockets, 2GB RAM).
## Tuning
Thresholds live in PostureMetrics.java (MAX_TILT_DEG, TOO_CLOSE_INDEX, SLOUCH_RATIO).
