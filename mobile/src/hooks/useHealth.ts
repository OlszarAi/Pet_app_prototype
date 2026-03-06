import { useQuery }  from "@tanstack/react-query";
import { fetchHealth } from "../api/health";
import { use } from "react";

export function useHealth() {
    return useQuery({
        queryKey: ["health"],
        queryFn: fetchHealth,
        refetchInterval: 10000, // Refetch every 10 seconds
    });
}