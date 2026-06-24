#!/bin/bash

# LIMS系统监控部署脚本
# 作者: Stacky Database Reviewer
# 描述: 一键部署Grafana + Prometheus + Alertmanager监控栈

set -e

MONITORING_COMPOSE_FILE="docker-compose.monitoring.yml"
MAIN_COMPOSE_FILE="docker-compose.yml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示使用说明
show_usage() {
    echo "LIMS监控系统部署脚本"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|clean}"
    echo ""
    echo "命令说明:"
    echo "  start     - 启动监控服务"
    echo "  stop      - 停止监控服务"
    echo "  restart   - 重启监控服务"
    echo "  status    - 查看服务状态"
    echo "  logs      - 查看服务日志"
    echo "  clean     - 清理监控数据（谨慎使用）"
    echo ""
    echo "访问地址:"
    echo "  Grafana仪表板: http://localhost:3000 (admin/admin123)"
    echo "  Prometheus: http://localhost:9090"
    echo "  Alertmanager: http://localhost:9093"
    echo "  Node Exporter: http://localhost:9100"
}

# 检查Docker Compose文件是否存在
check_compose_files() {
    if [ ! -f "$MONITORING_COMPOSE_FILE" ]; then
        log_error "监控配置文件 $MONITORING_COMPOSE_FILE 不存在"
        exit 1
    fi
    
    if [ ! -f "$MAIN_COMPOSE_FILE" ]; then
        log_warning "主应用配置文件 $MAIN_COMPOSE_FILE 不存在，仅启动监控服务"
    fi
}

# 启动监控服务
start_monitoring() {
    log_info "正在启动LIMS监控服务..."
    
    check_compose_files
    
    # 创建监控网络（如果不存在）
    if ! docker network ls | grep -q lims-monitoring; then
        log_info "创建监控网络..."
        docker network create lims-monitoring
    fi
    
    # 启动监控服务
    log_info "启动Prometheus、Grafana、Alertmanager..."
    docker-compose -f "$MONITORING_COMPOSE_FILE" up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    check_services_status
    
    log_success "监控服务启动完成！"
    echo ""
    show_access_info
}

# 停止监控服务
stop_monitoring() {
    log_info "正在停止监控服务..."
    
    if [ -f "$MONITORING_COMPOSE_FILE" ]; then
        docker-compose -f "$MONITORING_COMPOSE_FILE" down
        log_success "监控服务已停止"
    else
        log_error "监控配置文件不存在"
    fi
}

# 重启监控服务
restart_monitoring() {
    log_info "正在重启监控服务..."
    stop_monitoring
    sleep 5
    start_monitoring
}

# 检查服务状态
check_services_status() {
    log_info "检查服务状态..."
    
    services=("lims-prometheus" "lims-grafana" "lims-alertmanager" "lims-node-exporter")
    
    for service in "${services[@]}"; do
        if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q "$service"; then
            status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep "$service" | awk '{print $2}')
            echo -e "${GREEN}✓${NC} $service: $status"
        else
            echo -e "${RED}✗${NC} $service: 未运行"
        fi
    done
}

# 查看服务日志
show_logs() {
    log_info "显示监控服务日志（Ctrl+C退出）..."
    docker-compose -f "$MONITORING_COMPOSE_FILE" logs -f
}

# 清理监控数据
clean_monitoring() {
    log_warning "此操作将删除所有监控数据，包括历史指标和仪表板配置"
    read -p "确定要继续吗？(y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "正在清理监控数据..."
        
        # 停止服务
        docker-compose -f "$MONITORING_COMPOSE_FILE" down
        
        # 删除数据卷
        docker volume rm material-lims_prometheus_data 2>/dev/null || true
        docker volume rm material-lims_grafana_data 2>/dev/null || true
        docker volume rm material-lims_alertmanager_data 2>/dev/null || true
        
        # 删除监控网络
        docker network rm lims-monitoring 2>/dev/null || true
        
        log_success "监控数据清理完成"
    else
        log_info "操作已取消"
    fi
}

# 显示访问信息
show_access_info() {
    echo "=========================================="
    echo "          LIMS监控系统访问信息"
    echo "=========================================="
    echo "Grafana仪表板: http://localhost:3000"
    echo "  用户名: admin"
    echo "  密码: admin123"
    echo ""
    echo "Prometheus监控: http://localhost:9090"
    echo "Alertmanager告警: http://localhost:9093"
    echo "Node Exporter主机监控: http://localhost:9100"
    echo "=========================================="
}

# 主程序
case "$1" in
    start)
        start_monitoring
        ;;
    stop)
        stop_monitoring
        ;;
    restart)
        restart_monitoring
        ;;
    status)
        check_services_status
        ;;
    logs)
        show_logs
        ;;
    clean)
        clean_monitoring
        ;;
    *)
        show_usage
        exit 1
        ;;
esac