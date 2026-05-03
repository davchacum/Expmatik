import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";

const WS_PATH = "/ws";
const UNREAD_COUNT_ENDPOINT = "/api/notifications/unread/count";
const WS_BASE_URL = import.meta.env.VITE_WS_URL ?? "";
const UNREAD_COUNT_TOPIC = "/user/queue/notifications/unread-count";

export const useNotificationUnreadCount = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const accessToken = localStorage.getItem("accessToken");

  useEffect(() => {
    if (!accessToken) return undefined;

    const fetchUnreadCount = async () => {
      try {
        const response = await fetch(UNREAD_COUNT_ENDPOINT, {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (!response.ok) return;

        const count = await response.json();
        setUnreadCount(Number.isFinite(count) ? count : 0);
      } catch {
        console.error("Failed to fetch unread notification count");
      }
    };

    fetchUnreadCount();

    const wsBase = WS_BASE_URL || `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}`;
    const wsUrl = `${wsBase}${WS_PATH}`;

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
    });

    client.onConnect = () => {
      client.subscribe(UNREAD_COUNT_TOPIC, (message) => {
        const newCount = Number(message.body);
        if (!Number.isNaN(newCount)) {
          setUnreadCount(newCount);
        }
      });
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [accessToken]);

  return accessToken ? unreadCount : 0;
};
