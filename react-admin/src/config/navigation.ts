import {
  Activity,
  Binary,
  CarFront,
  Calculator,
  Factory,
  FileSearch,
  FileText,
  LayoutDashboard,
  Network,
  PanelLeft,
  Tags,
  Table2,
  type LucideIcon,
} from "lucide-react";
import { getMenuIcon } from "@/lib/menu-icons";
import type { CurrentUserMenu } from "@/types";

export type NavItem = {
  label: string;
  path: string;
  icon: LucideIcon;
  externalUrl?: string;
  activePaths?: string[];
  children?: NavItem[];
};

export const defaultNavItems: NavItem[] = [
  {
    label: "Dashboard",
    path: "/dashboard",
    icon: LayoutDashboard,
  },

  {
    label: "页面示例",
    path: "/examples",
    icon: FileText,
    children: [
      {
        label: "表单示例",
        path: "/forms/basic",
        icon: FileText,
      },
      {
        label: "列表页 Demo",
        path: "/examples/list",
        icon: Table2,
      },
      {
        label: "树形结构 Demo",
        path: "/examples/tree",
        icon: Network,
      },
      {
        label: "左树右表 Demo",
        path: "/examples/tree-table",
        icon: PanelLeft,
      },
      {
        label: "详情页 Demo",
        path: "/examples/detail",
        icon: FileSearch,
      },
    ],
  },
  {
    label: "GCAN",
    path: "/gcan",
    icon: CarFront,
    children: [
      {
        label: "车辆档案",
        path: "/gcan/vehicles",
        icon: CarFront,
      },
      {
        label: "煤矿维护",
        path: "/gcan/mines",
        icon: Factory,
      },
      {
        label: "车型管理",
        path: "/gcan/vehicle-types",
        icon: Tags,
      },
      {
        label: "车辆 CAN 状态",
        path: "/gcan/vehicle-can-state",
        icon: Activity,
      },
      {
        label: "HEX/DEC 转换",
        path: "/gcan/hex-dec",
        icon: Calculator,
      },
      {
        label: "原始 CAN 帧",
        path: "/gcan/raw-frames",
        icon: Binary,
      },
    ],
  },
];

function toNavItem(menu: CurrentUserMenu): NavItem | null {
  if (menu.visible !== 1) return null;

  const sortedChildren = [...(menu.children ?? [])].sort(
    (a, b) => a.sortOrder - b.sortOrder,
  );
  const children = sortedChildren
    .map((child) => toNavItem(child))
    .filter((item): item is NavItem => Boolean(item));

  if (menu.menuType === "DIR" && children.length === 0) return null;

  if (menu.menuType === "LINK" && !menu.externalUrl) return null;

  const externalUrl = menu.menuType === "LINK" ? menu.externalUrl : "";
  const path = externalUrl || menu.path;

  if (!path) return null;

  return {
    label: menu.menuName,
    path,
    icon: getMenuIcon(menu.icon),
    externalUrl: externalUrl || undefined,
    children: children.length > 0 ? children : undefined,
  };
}

export function convertUserMenusToNavItems(menus: CurrentUserMenu[]): NavItem[] {
  return [...menus]
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((menu) => toNavItem(menu))
    .filter((item): item is NavItem => Boolean(item));
}

export function mergeNavItems(
  baseItems: NavItem[],
  userItems: NavItem[],
): NavItem[] {
  const userItemsByPath = new Map(
    userItems.map((item) => [item.externalUrl ?? item.path, item]),
  );
  const mergedBaseItems = baseItems.map((baseItem) => {
    const userItem = userItemsByPath.get(baseItem.externalUrl ?? baseItem.path);
    if (!userItem) return baseItem;

    return {
      ...baseItem,
      children: mergeNavItems(baseItem.children ?? [], userItem.children ?? []),
    };
  });
  const basePaths = new Set(
    baseItems.map((item) => item.externalUrl ?? item.path),
  );

  return [
    ...mergedBaseItems,
    ...userItems.filter(
      (item) => !basePaths.has(item.externalUrl ?? item.path),
    ),
  ];
}

export function createUserMenuTitleMap(
  menus: CurrentUserMenu[],
): Record<string, string> {
  const titleMap: Record<string, string> = {};

  const walk = (items: CurrentUserMenu[]) => {
    items.forEach((item) => {
      if (item.visible !== 1) return;

      const path = item.menuType === "LINK" ? item.externalUrl : item.path;
      if (path) titleMap[path] = item.menuName;
      if (item.children?.length) walk(item.children);
    });
  };

  walk(menus);
  return titleMap;
}

export const staticRouteTitleMap: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/forms/basic": "表单示例",
  "/examples": "页面示例",
  "/examples/list": "列表页 Demo",
  "/examples/tree": "树形结构 Demo",
  "/examples/tree-table": "左树右表 Demo",
  "/examples/detail": "详情页 Demo",
  "/settings": "系统设置",
  "/gcan": "GCAN",
  "/gcan/vehicles": "车辆档案",
  "/gcan/mines": "煤矿维护",
  "/gcan/vehicle-types": "车型管理",
  "/gcan/vehicle-can-state": "车辆 CAN 状态",
  "/gcan/hex-dec": "HEX/DEC 转换",
  "/gcan/raw-frames": "原始 CAN 帧",
  "/system": "系统管理",
  "/system/user": "用户管理",
  "/system/dept": "部门管理",
  "/system/dict": "字典管理",
  "/system/config": "配置管理",
  "/system/role": "角色管理",
  "/system/menu": "菜单管理",
  "/system/login-log": "登录日志",
  "/system/oper-log": "操作日志",
  "/system/file": "文件管理",
  "/account/profile": "个人中心",
  "/account/change-password": "修改密码",
};

export const navItems = defaultNavItems;
export const routeTitleMap = staticRouteTitleMap;
