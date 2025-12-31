import axios from 'axios';

// 기본 API 클라이언트 설정
const client = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

export default client;