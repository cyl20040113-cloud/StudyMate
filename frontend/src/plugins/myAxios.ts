import axios, { type AxiosError, type AxiosRequestConfig } from "axios";

import { Toast } from "vant";
import type { BaseResponse } from "../types/api";

interface HttpClient {
  get<T = unknown>(
    url: string,
    config?: AxiosRequestConfig,
  ): Promise<BaseResponse<T>>;

  post<T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>,
  ): Promise<BaseResponse<T>>;
}

const axiosInstance = axios.create({
  baseURL: "/api",
  withCredentials: true,
  timeout: 10000,
});

/**
 * 请求拦截器
 */
axiosInstance.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error),
);

/**
 * 响应拦截器
 */
axiosInstance.interceptors.response.use(
  (response) => response,

  (error: AxiosError) => {
    let message = "请求失败，请稍后重试";

    // 请求超时
    if (error.code === "ECONNABORTED") {
      message = "请求超时，请稍后重试";
    }

    // 完全无法连接后端
    else if (!error.response) {
      message = "无法连接服务器，请检查后端是否启动";
    }

    // HTTP 状态码错误
    else {
      const status = error.response.status;

      if (status === 401) {
        message = "登录状态已失效，请重新登录";
      } else if (status === 403) {
        message = "没有权限执行该操作";
      } else if (status === 404) {
        message = "请求接口不存在";
      } else if (status >= 500) {
        message = "服务器异常，请稍后重试";
      }
    }

    Toast.fail(message);

    console.error("Axios 请求异常：", error);

    return Promise.reject(error);
  },
);

const myAxios: HttpClient = {
  async get<T = unknown>(
    url: string,
    config?: AxiosRequestConfig,
  ): Promise<BaseResponse<T>> {
    const response = await axiosInstance.get<BaseResponse<T>>(url, config);
    return response.data;
  },

  async post<T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>,
  ): Promise<BaseResponse<T>> {
    const response = await axiosInstance.post<BaseResponse<T>>(
      url,
      data,
      config,
    );

    return response.data;
  },
};

export default myAxios;
