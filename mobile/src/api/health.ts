import apiClient from "./client";

// { success: true, data: { status: "UP" } }
type HealthResponse = {
    data: {
        status: string;
    };
};

export async function fetchHealth(): Promise<{status: string}> {
    const BASE = process.env.EXPO_PUBLIC_API_URL
}