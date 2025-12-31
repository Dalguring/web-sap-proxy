import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import client from '../api/client';
import type {InterfaceDefinition} from '../types/interface';

interface SimpleProxyResponse {
    success: boolean;
    message?: string;
    data: any;
}

const InterfaceList = () => {
    const [interfaces, setInterfaces] = useState<InterfaceDefinition[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        void fetchInterfaces();
    }, []);

    const fetchInterfaces = async () => {
        try {
            const response = await client.get<SimpleProxyResponse>('/proxy/interfaces');
            const result = response.data;

            if (result.success && result.data && result.data['interfaces']) {
                const interfacesMap = result.data['interfaces'] as Record<string, InterfaceDefinition>;
                const interfacesList = Object.values(interfacesMap);
                setInterfaces(interfacesList);
            } else {
                setInterfaces([]);
            }
        } catch (err) {
            console.error('Failed to fetch interfaces', err);
            setError('인터페이스 목록을 불러오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="p-8 text-center">로딩 중...</div>;
    if (error) return <div className="p-8 text-center text-red-500">{error}</div>;

    return (
            <div className="bg-white shadow rounded-lg overflow-hidden">
                <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                    <h2 className="text-xl font-bold text-gray-800">인터페이스 목록</h2>
                    <Link
                            to="/new"
                            className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded transition duration-150"
                    >
                        + 신규 생성
                    </Link>
                </div>

                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Interface
                                Name
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">RFC
                                Function
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                        {interfaces.length === 0 ? (
                                <tr>
                                    <td colSpan={5}
                                        className="px-6 py-10 text-center text-gray-500">
                                        등록된 인터페이스가 없습니다.
                                    </td>
                                </tr>
                        ) : (
                                interfaces.map((iface) => (
                                        <tr key={iface.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600">
                                                <Link to={`/edit/${iface.id}`}>{iface.id}</Link>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{iface.name}</td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">{iface.rfcFunction}</td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{iface.description}</td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                                <Link to={`/edit/${iface.id}`}
                                                      className="text-indigo-600 hover:text-indigo-900 mr-4">수정</Link>
                                                <button className="text-red-600 hover:text-red-900">삭제</button>
                                            </td>
                                        </tr>
                                ))
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
    );
};

export default InterfaceList;