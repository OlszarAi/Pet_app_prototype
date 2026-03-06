import apiClient from "./client";

// { success: true, data: { status: "UP" } }
type HealthResponse = {
    data: {
        status: string;
    };
};

export async function fetchHealth(): Promise<{status: string}> {
    const response = await apiClient.get<HealthResponse>("/health");
    return response.data.data; 
}