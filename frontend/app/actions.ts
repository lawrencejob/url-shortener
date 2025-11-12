'use server'
 
import { z } from 'zod'
 
const schema = z.object({
    fullUrl: z
        .url()
        .trim()
        .refine((val) => /^https?:\/\//.test(val), {
            message: "URL must start with http:// or https://",
        }),
    alias: z
        .string()
        .trim()
        .nullish()
        .optional()
        .refine(
            (val) => !val || /^[a-zA-Z0-9]{3,32}$/.test(val),
            "Alias must be 3–32 characters of a-z, A-Z or 0-9"
        ),
})
 
export async function shortenAction(previousState: any, formData: FormData) {
    console.log("Action called with:", previousState, formData)
  
  const validatedFields = schema.safeParse({
    fullUrl: formData.get('fullUrl'),
    alias: formData.get('alias'),
  })
 
  // Return early if the form data is invalid
  if (!validatedFields.success) {
    return {
      errors: z.treeifyError(validatedFields.error),
    }
  }

  //const result = await shortenUrl(parsed.data)
  return { shortUrl: "..." }
}