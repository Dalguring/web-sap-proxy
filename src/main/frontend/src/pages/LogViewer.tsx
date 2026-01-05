import {useState, useEffect} from 'react';
import client from '../api/client';

interface ModuleStats {
    sapModule: string;
    totalCount: number;
    successCount: number;
    failCount: number;
}

interface InterfaceStats {
    interfaceId: string;
    rfcFunction: string;
    totalCount: number;
    successCount: number;
    failCount: number;
}

interface ErrorLog {
    id: number;
    requestId: string;
    errorMessage: string;
    requestData: string; // JSON String
    createdAt: string;
}

const LogViewer = () => {
    // 오늘 날짜로 초기화 (YYYY-MM-DD)
    const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

    const [moduleStats, setModuleStats] = useState<ModuleStats[]>([]);
    const [selectedModule, setSelectedModule] = useState<string | null>(null);

    const [interfaceStats, setInterfaceStats] = useState<InterfaceStats[]>([]);

    // 에러 로그 모달 상태
    const [errorLogs, setErrorLogs] = useState<ErrorLog[]>([]);
    const [showModal, setShowModal] = useState(false);
    const [selectedInterface, setSelectedInterface] = useState('');

    // 날짜가 바뀌면 모듈 통계 다시 로드
    useEffect(() => {
        fetchModuleStats();
        setSelectedModule(null);
        setInterfaceStats([]);
    }, [date]);

    // 모듈이 선택되면 인터페이스 통계 로드
    useEffect(() => {
        if (selectedModule) {
            fetchInterfaceStats(selectedModule);
        }
    }, [selectedModule]);

    const fetchModuleStats = async () => {
        try {
            const res = await client.get(`/admin/stats/daily?date=${date}`);
            if (res.data.success && res.data.data) {
                setModuleStats(res.data.data.stats || []);
            }
        } catch (e) {
            console.error("통계 로딩 실패", e);
            setModuleStats([]);
        }
    };

    const fetchInterfaceStats = async (module: string) => {
        try {
            const res = await client.get(`/admin/stats/module?date=${date}&module=${module}`);
            if (res.data.success && res.data.data) {
                setInterfaceStats(res.data.data.stats || []);
            }
        } catch (e) {
            console.error("상세 통계 로딩 실패", e);
            setInterfaceStats([]);
        }
    };

    const fetchErrorLogs = async (interfaceId: string) => {
        try {
            const res = await client.get(`/admin/stats/errors?date=${date}&interfaceId=${interfaceId}`);
            if (res.data.success && res.data.data) {
                setErrorLogs(res.data.data.logs || []);
                setSelectedInterface(interfaceId);
                setShowModal(true);
            }
        } catch (e) {
            alert('로그 조회 실패');
        }
    };

    return (
            <div className="bg-white shadow-lg rounded-lg min-h-[80vh] p-6">
                <h1 className="text-2xl font-bold text-gray-800 mb-6">📊 인터페이스 실행 로그</h1>

                {/* 날짜 선택기 */}
                <div className="mb-8 flex items-center gap-4 bg-gray-50 p-4 rounded border">
                    <label className="font-bold text-gray-700">조회 일자:</label>
                    <input
                            type="date"
                            value={date}
                            onChange={(e) => setDate(e.target.value)}
                            className="border rounded px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* [좌측] 모듈별 통계 리스트 */}
                    <div className="col-span-1">
                        <h3 className="text-lg font-bold text-gray-700 mb-3 border-l-4 border-blue-500 pl-2">모듈별
                            현황</h3>
                        <div className="space-y-3">
                            {moduleStats.length === 0 ? (
                                    <div className="text-gray-400 text-sm">데이터가 없습니다.</div>
                            ) : (
                                    moduleStats.map((stat) => (
                                            <div
                                                    key={stat.sapModule}
                                                    onClick={() => setSelectedModule(stat.sapModule)}
                                                    className={`p-4 rounded-lg border cursor-pointer transition-all hover:shadow-md ${selectedModule === stat.sapModule ? 'bg-blue-50 border-blue-500 ring-2 ring-blue-200' : 'bg-white border-gray-200'}`}
                                            >
                                                <div className="flex justify-between items-center mb-2">
                                                    <span className="font-bold text-lg text-gray-800">{stat.sapModule}</span>
                                                    <span className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full">Total: {stat.totalCount}</span>
                                                </div>
                                                <div className="flex gap-2 text-sm">
                                                    <span className="flex-1 bg-green-100 text-green-700 px-2 py-1 rounded text-center">성공 {stat.successCount}</span>
                                                    <span className="flex-1 bg-red-100 text-red-700 px-2 py-1 rounded text-center">실패 {stat.failCount}</span>
                                                </div>
                                            </div>
                                    ))
                            )}
                        </div>
                    </div>

                    {/* [우측] 인터페이스 상세 통계 테이블 */}
                    <div className="col-span-2">
                        <h3 className="text-lg font-bold text-gray-700 mb-3 border-l-4 border-purple-500 pl-2">
                            {selectedModule ? `'${selectedModule}' 모듈 상세 내역` : '모듈을 선택해주세요'}
                        </h3>

                        {selectedModule && (
                                <div className="overflow-x-auto border rounded-lg">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Interface
                                                ID
                                            </th>
                                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">RFC
                                                Function
                                            </th>
                                            <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Total</th>
                                            <th className="px-4 py-3 text-center text-xs font-medium text-green-600 uppercase">Success</th>
                                            <th className="px-4 py-3 text-center text-xs font-medium text-red-600 uppercase">Fail</th>
                                        </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {interfaceStats.length === 0 ? (
                                                <tr>
                                                    <td colSpan={5}
                                                        className="px-4 py-8 text-center text-gray-400">실행
                                                        이력이 없습니다.
                                                    </td>
                                                </tr>
                                        ) : (
                                                interfaceStats.map((stat) => (
                                                        <tr key={stat.interfaceId}
                                                            className="hover:bg-gray-50">
                                                            <td className="px-4 py-3 text-sm font-medium text-gray-900">{stat.interfaceId}</td>
                                                            <td className="px-4 py-3 text-sm text-gray-500 font-mono">{stat.rfcFunction}</td>
                                                            <td className="px-4 py-3 text-center text-sm font-bold">{stat.totalCount}</td>
                                                            <td className="px-4 py-3 text-center text-sm text-green-600">{stat.successCount}</td>
                                                            <td className="px-4 py-3 text-center text-sm">
                                                                {stat.failCount > 0 ? (
                                                                        <button
                                                                                onClick={() => fetchErrorLogs(stat.interfaceId)}
                                                                                className="bg-red-100 text-red-700 px-3 py-1 rounded-full font-bold hover:bg-red-200 underline"
                                                                        >
                                                                            {stat.failCount}
                                                                        </button>
                                                                ) : (
                                                                        <span className="text-gray-300">0</span>
                                                                )}
                                                            </td>
                                                        </tr>
                                                ))
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                        )}
                    </div>
                </div>

                {/* 에러 로그 모달 */}
                {showModal && (
                        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
                                <div className="p-4 border-b bg-red-50 flex justify-between items-center">
                                    <h3 className="font-bold text-red-800">🚨 실패 로그 상세
                                        ({selectedInterface})</h3>
                                    <button onClick={() => setShowModal(false)}
                                            className="text-gray-500 hover:text-black text-2xl">×
                                    </button>
                                </div>
                                <div className="p-4 overflow-y-auto flex-1 space-y-4">
                                    {errorLogs.length === 0 ? (
                                            <div className="text-center text-gray-500">표시할 내용이
                                                없습니다.</div>
                                    ) : (
                                            errorLogs.map((log) => (
                                                    <div key={log.id}
                                                         className="border border-red-200 rounded p-3 bg-red-50/30">
                                                        <div className="flex justify-between text-xs text-gray-500 mb-1">
                                                            <span>Time: {new Date(log.createdAt).toLocaleString()}</span>
                                                            <span>ReqID: {log.requestId}</span>
                                                        </div>
                                                        <div className="text-red-700 font-semibold mb-2">{log.errorMessage}</div>
                                                        <details>
                                                            <summary
                                                                    className="cursor-pointer text-xs text-blue-600 hover:underline">요청
                                                                데이터 보기 (Payload)
                                                            </summary>
                                                            <pre className="mt-2 bg-gray-800 text-green-400 p-2 rounded text-xs overflow-x-auto whitespace-pre-wrap break-all">
                                                {/* JSON 포맷팅 시도 */}
                                                                {(() => {
                                                                    try {
                                                                        return JSON.stringify(JSON.parse(log.requestData), null, 2);
                                                                    } catch (e) {
                                                                        return log.requestData;
                                                                    }
                                                                })()}
                                            </pre>
                                                        </details>
                                                    </div>
                                            ))
                                    )}
                                </div>
                                <div className="p-3 border-t bg-gray-50 text-right">
                                    <button onClick={() => setShowModal(false)}
                                            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700">닫기
                                    </button>
                                </div>
                            </div>
                        </div>
                )}
            </div>
    );
};

export default LogViewer;