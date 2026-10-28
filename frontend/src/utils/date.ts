export const formatDateTime = (
  value?: string | Date | null
): string => {

  if (!value) {
    return '长期有效';
  }

  const date =
    value instanceof Date
      ? value
      : new Date(value);

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return '时间未知';
  }

  const year =
    date.getFullYear();

  const month =
    String(
      date.getMonth() + 1
    ).padStart(2, '0');

  const day =
    String(
      date.getDate()
    ).padStart(2, '0');

  const hour =
    String(
      date.getHours()
    ).padStart(2, '0');

  const minute =
    String(
      date.getMinutes()
    ).padStart(2, '0');

  return `${year}-${month}-${day} ${hour}:${minute}`;
};