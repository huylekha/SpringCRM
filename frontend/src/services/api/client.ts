import axios from "axios";
import { setupInterceptors } from "./interceptors";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

setupInterceptors(apiClient);

export default apiClient;
